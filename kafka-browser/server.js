const http = require('http');
const fs = require('fs');
const path = require('path');
const os = require('os');
const crypto = require('crypto');
const { execFile } = require('child_process');
const { WebSocketServer } = require('ws');
const forge = require('node-forge');
const jks = require('jks-js');
const { Kafka, logLevel } = require('kafkajs');

const PORT = process.env.PORT ? Number(process.env.PORT) : 3000;
const HOST = process.env.HOST || '127.0.0.1';
const INDEX_PATH = path.join(__dirname, 'index.html');

const MAX_QUEUE = 1000;
const BUFFERED_AMOUNT_HIGH = 5 * 1024 * 1024;
const BUFFERED_AMOUNT_LOW = 512 * 1024;

function safeJsonParse(value, fallback) {
  try {
    return JSON.parse(value);
  } catch (error) {
    return fallback;
  }
}

function parseBase64(base64) {
  return Buffer.from(base64, 'base64');
}

function bufferToUtf8(buffer) {
  const text = buffer.toString('utf8');
  if (Buffer.from(text, 'utf8').equals(buffer)) {
    return { text, encoding: 'utf8' };
  }
  return { text: buffer.toString('base64'), encoding: 'base64' };
}

function redactConfig(config) {
  return {
    brokers: config.brokers,
    clientId: config.clientId,
    tls: config.tls ? { enabled: config.tls.enabled } : undefined,
    sasl: config.sasl ? { enabled: config.sasl.enabled, mechanism: config.sasl.mechanism } : undefined,
    topic: config.topic,
    from: config.from
  };
}

function parsePkcs12(buffer, storePassword, keyPassword) {
  const p12Asn1 = forge.asn1.fromDer(buffer.toString('binary'));
  const p12 = forge.pkcs12.pkcs12FromAsn1(p12Asn1, false, storePassword || '');
  const certBags = p12.getBags({ bagType: forge.pki.oids.certBag })[forge.pki.oids.certBag] || [];
  const keyBags = p12.getBags({ bagType: forge.pki.oids.pkcs8ShroudedKeyBag })[forge.pki.oids.pkcs8ShroudedKeyBag] || [];
  const fallbackKeyBags = p12.getBags({ bagType: forge.pki.oids.keyBag })[forge.pki.oids.keyBag] || [];
  const keys = keyBags.length ? keyBags : fallbackKeyBags;

  const ca = certBags.map((bag) => forge.pki.certificateToPem(bag.cert));
  const keyBag = keys[0];
  const key = keyBag ? forge.pki.privateKeyToPem(keyBag.key) : null;
  const cert = certBags[0] ? forge.pki.certificateToPem(certBags[0].cert) : null;

  if (keyPassword && key) {
    return { ca, key, cert };
  }

  return { ca, key, cert };
}

function parseJks(buffer, storePassword) {
  const keystore = jks.parse(buffer, storePassword || '');
  const ca = [];
  let key = null;
  let cert = null;

  Object.values(keystore).forEach((entry) => {
    if (entry.type === 'trustedCert') {
      ca.push(forge.pki.certificateToPem(entry.cert));
    }
    if (entry.type === 'privateKey') {
      if (entry.certChain && entry.certChain.length) {
        cert = forge.pki.certificateToPem(entry.certChain[0].cert);
        entry.certChain.forEach((chain) => {
          ca.push(forge.pki.certificateToPem(chain.cert));
        });
      }
      if (entry.key) {
        key = forge.pki.privateKeyToPem(entry.key);
      }
    }
  });

  return { ca, key, cert };
}

function execFileAsync(command, args, options) {
  return new Promise((resolve, reject) => {
    execFile(command, args, options, (error, stdout, stderr) => {
      if (error) {
        reject(new Error(stderr || error.message));
        return;
      }
      resolve(stdout);
    });
  });
}

async function keytoolAvailable() {
  try {
    await execFileAsync('keytool', ['-help'], { timeout: 5000 });
    return true;
  } catch (error) {
    return false;
  }
}

async function convertJksToPkcs12(buffer, storePassword, keyPassword) {
  const tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), 'kafka-browser-'));
  const jksPath = path.join(tmpDir, 'store.jks');
  const p12Path = path.join(tmpDir, 'store.p12');
  fs.writeFileSync(jksPath, buffer);

  const args = [
    '-importkeystore',
    '-srckeystore',
    jksPath,
    '-destkeystore',
    p12Path,
    '-deststoretype',
    'PKCS12',
    '-srcstorepass',
    storePassword || '',
    '-deststorepass',
    storePassword || ''
  ];
  if (keyPassword) {
    args.push('-srckeypass', keyPassword);
  }

  try {
    await execFileAsync('keytool', args, { timeout: 10000 });
    const p12Buffer = fs.readFileSync(p12Path);
    return parsePkcs12(p12Buffer, storePassword, keyPassword);
  } finally {
    fs.rmSync(tmpDir, { recursive: true, force: true });
  }
}

async function parseStore({ bytesBase64, filename, storePassword, keyPassword, storeType }) {
  const buffer = parseBase64(bytesBase64);
  const ext = path.extname(filename || '').toLowerCase();
  const effectiveType = storeType || (ext === '.p12' || ext === '.pfx' ? 'pkcs12' : 'jks');

  if (effectiveType === 'pkcs12') {
    return parsePkcs12(buffer, storePassword, keyPassword);
  }

  try {
    return parseJks(buffer, storePassword);
  } catch (error) {
    if (await keytoolAvailable()) {
      return convertJksToPkcs12(buffer, storePassword, keyPassword);
    }
    throw error;
  }
}

async function buildSslOptions(tlsConfig) {
  if (!tlsConfig || !tlsConfig.enabled) {
    return undefined;
  }

  const ssl = { rejectUnauthorized: true };

  if (tlsConfig.truststore && tlsConfig.truststore.bytesBase64) {
    const trust = await parseStore({
      bytesBase64: tlsConfig.truststore.bytesBase64,
      filename: tlsConfig.truststore.filename,
      storePassword: tlsConfig.truststore.storePassword,
      storeType: tlsConfig.truststore.storeType
    });
    if (trust.ca && trust.ca.length) {
      ssl.ca = trust.ca;
    }
  }

  if (tlsConfig.keystore && tlsConfig.keystore.bytesBase64) {
    const keyStore = await parseStore({
      bytesBase64: tlsConfig.keystore.bytesBase64,
      filename: tlsConfig.keystore.filename,
      storePassword: tlsConfig.keystore.storePassword,
      keyPassword: tlsConfig.keystore.keyPassword,
      storeType: tlsConfig.keystore.storeType
    });
    if (keyStore.key) {
      ssl.key = keyStore.key;
    }
    if (keyStore.cert) {
      ssl.cert = keyStore.cert;
    }
    if (keyStore.ca && keyStore.ca.length) {
      ssl.ca = (ssl.ca || []).concat(keyStore.ca);
    }
  }

  return ssl;
}

function buildSaslOptions(saslConfig) {
  if (!saslConfig || !saslConfig.enabled) {
    return undefined;
  }
  if (!saslConfig.mechanism || !saslConfig.username || !saslConfig.password) {
    throw new Error('SASL requires mechanism, username, and password.');
  }
  return {
    mechanism: saslConfig.mechanism,
    username: saslConfig.username,
    password: saslConfig.password
  };
}

function validateConnectConfig(config) {
  if (!config || !config.brokers || !config.brokers.length) {
    throw new Error('Brokers are required.');
  }
  return true;
}

function createServer() {
  const server = http.createServer((req, res) => {
    if (req.url === '/' || req.url === '/index.html') {
      const html = fs.readFileSync(INDEX_PATH);
      res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
      res.end(html);
      return;
    }
    res.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' });
    res.end('Not found');
  });

  const wss = new WebSocketServer({ server, path: '/ws' });

  wss.on('connection', (ws) => {
    const state = {
      kafka: null,
      producer: null,
      consumer: null,
      admin: null,
      config: null,
      topic: null,
      from: 'latest',
      queue: [],
      dropped: 0,
      flushTimer: null,
      groupId: `kafkadrop-ui-${crypto.randomBytes(4).toString('hex')}`
    };

    const send = (payload) => {
      if (ws.readyState !== ws.OPEN) {
        return;
      }
      const message = JSON.stringify(payload);
      if (ws.bufferedAmount > BUFFERED_AMOUNT_HIGH) {
        state.queue.push(message);
        if (state.queue.length > MAX_QUEUE) {
          const removed = state.queue.splice(0, state.queue.length - MAX_QUEUE);
          state.dropped += removed.length;
          sendStatus('consuming', `Dropped ${state.dropped} messages due to backpressure.`);
        }
        scheduleFlush();
        return;
      }
      ws.send(message);
    };

    const scheduleFlush = () => {
      if (state.flushTimer) {
        return;
      }
      state.flushTimer = setInterval(() => {
        if (ws.readyState !== ws.OPEN) {
          clearInterval(state.flushTimer);
          state.flushTimer = null;
          return;
        }
        if (ws.bufferedAmount > BUFFERED_AMOUNT_LOW) {
          return;
        }
        while (state.queue.length && ws.bufferedAmount <= BUFFERED_AMOUNT_LOW) {
          ws.send(state.queue.shift());
        }
        if (!state.queue.length) {
          clearInterval(state.flushTimer);
          state.flushTimer = null;
        }
      }, 200);
    };

    const sendStatus = (status, details) => {
      send({ type: 'status', state: status, details });
    };

    const cleanupKafka = async () => {
      if (state.consumer) {
        try {
          await state.consumer.stop();
          await state.consumer.disconnect();
        } catch (error) {
          // ignore
        }
        state.consumer = null;
      }
      if (state.producer) {
        try {
          await state.producer.disconnect();
        } catch (error) {
          // ignore
        }
        state.producer = null;
      }
      if (state.admin) {
        try {
          await state.admin.disconnect();
        } catch (error) {
          // ignore
        }
        state.admin = null;
      }
      state.kafka = null;
    };

    ws.on('message', async (raw) => {
      const data = safeJsonParse(raw.toString(), null);
      if (!data || !data.type) {
        send({ type: 'error', error: { message: 'Invalid message.' } });
        return;
      }

      try {
        if (data.type === 'connect') {
          validateConnectConfig(data.config);
          const config = data.config;
          state.topic = config.topic || null;
          state.from = config.from || 'latest';

          const ssl = await buildSslOptions(config.tls);
          const sasl = buildSaslOptions(config.sasl);

          state.kafka = new Kafka({
            clientId: config.clientId || `kafka-browser-${crypto.randomBytes(3).toString('hex')}`,
            brokers: config.brokers,
            ssl,
            sasl,
            logLevel: logLevel.NOTHING
          });
          state.producer = state.kafka.producer();
          await state.producer.connect();
          state.config = config;
          sendStatus('connected', 'Connected to broker.');
          return;
        }

        if (data.type === 'list_topics') {
          if (!state.kafka) {
            throw new Error('Not connected.');
          }
          if (!state.admin) {
            state.admin = state.kafka.admin();
            await state.admin.connect();
          }
          const topics = await state.admin.listTopics();
          send({ type: 'topics', topics });
          return;
        }

        if (data.type === 'start_consume') {
          if (!state.kafka) {
            throw new Error('Not connected.');
          }
          if (!state.topic) {
            throw new Error('Topic is required.');
          }
          if (state.consumer) {
            await state.consumer.stop();
            await state.consumer.disconnect();
          }
          state.consumer = state.kafka.consumer({ groupId: state.groupId });
          await state.consumer.connect();
          await state.consumer.subscribe({ topic: state.topic, fromBeginning: state.from === 'earliest' });
          await state.consumer.run({
            eachMessage: async ({ message, partition }) => {
              const key = message.key ? bufferToUtf8(message.key) : { text: '', encoding: 'utf8' };
              const value = message.value ? bufferToUtf8(message.value) : { text: '', encoding: 'utf8' };
              send({
                type: 'message',
                msg: {
                  timestamp: message.timestamp,
                  partition,
                  offset: message.offset,
                  key: key.text,
                  keyEncoding: key.encoding,
                  value: value.text,
                  valueEncoding: value.encoding
                }
              });
            }
          });
          sendStatus('consuming', `Consuming from ${state.topic}.`);
          return;
        }

        if (data.type === 'stop_consume') {
          if (state.consumer) {
            await state.consumer.stop();
            await state.consumer.disconnect();
            state.consumer = null;
          }
          sendStatus('stopped', 'Consumer stopped.');
          return;
        }

        if (data.type === 'produce') {
          if (!state.producer) {
            throw new Error('Not connected.');
          }
          const record = data.record || {};
          const topic = record.topic || state.topic;
          if (!topic) {
            throw new Error('Topic is required for produce.');
          }
          const message = {
            key: record.key || undefined,
            value: record.value
          };
          await state.producer.send({
            topic,
            messages: [message]
          });
          send({ type: 'produce_result', ok: true });
          return;
        }

        if (data.type === 'disconnect') {
          await cleanupKafka();
          sendStatus('idle', 'Disconnected.');
          return;
        }

        send({ type: 'error', error: { message: 'Unknown message type.' } });
      } catch (error) {
        console.error('Error handling message', redactConfig(state.config || {}), error.message);
        send({ type: 'error', error: { message: error.message || 'Unexpected error.' } });
        sendStatus('error', error.message);
      }
    });

    ws.on('close', async () => {
      if (state.flushTimer) {
        clearInterval(state.flushTimer);
        state.flushTimer = null;
      }
      await cleanupKafka();
    });

    sendStatus('idle', 'Ready.');
  });

  return server;
}

const server = createServer();
server.listen(PORT, HOST, () => {
  console.log(`Kafka browser listening on http://${HOST}:${PORT}`);
});
