export function parseJson(content) {
  const errors = [];
  try {
    return { data: JSON.parse(content), errors };
  } catch (error) {
    errors.push(error.message);
    return { data: null, errors };
  }
}
