function formatIssues(issues) {
  return issues.map((issue) => `- ${issue}`).join('\n');
}

export function formatBlockerReport({ fileSummaries, generalWarnings }) {
  const lines = ['Config Guard blocked stop due to blocker(s):'];
  for (const summary of fileSummaries) {
    lines.push(`\n${summary.file}:`);
    lines.push(formatIssues(summary.blockers));
  }
  if (generalWarnings.length > 0) {
    lines.push('\nAdditional warnings:');
    lines.push(formatIssues(generalWarnings));
  }
  lines.push('\nNext steps: fix the blockers and retry.');
  return lines.join('\n');
}
