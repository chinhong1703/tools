import { parseDocument } from 'yaml';

export function parseYaml(content) {
  const errors = [];
  try {
    const doc = parseDocument(content);
    if (doc.errors && doc.errors.length > 0) {
      doc.errors.forEach((err) => {
        errors.push(err.message);
      });
    }
    return { data: doc.toJS(), errors };
  } catch (error) {
    errors.push(error.message);
    return { data: null, errors };
  }
}
