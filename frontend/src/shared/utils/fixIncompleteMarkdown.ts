/**
 * 修复流式输出中不完整的 Markdown 语法，防止未闭合标记导致渲染异常。
 * 仅用于渲染展示，不修改原始累积文本。
 */
export default function fixIncompleteMarkdown(text: string): string {
  let result = text;

  // 1. 修复未闭合的代码围栏 (```)
  const fenceMatches = result.match(/^```/gm);
  if (fenceMatches && fenceMatches.length % 2 !== 0) {
    result += '\n```';
  }

  // 2. 修复未闭合的块级 LaTeX ($$)
  const blockLatexMatches = result.match(/\$\$/g);
  if (blockLatexMatches && blockLatexMatches.length % 2 !== 0) {
    result += '$$';
  }

  // 如果当前在未闭合的代码块内，不处理行内格式（代码块内的 * ` $ 是字面量）
  if (fenceMatches && fenceMatches.length % 2 !== 0) {
    return result;
  }

  // 3. 修复最后一行中未闭合的行内格式
  const lastNewline = result.lastIndexOf('\n');
  const lastLine = result.slice(lastNewline + 1);

  let fixedLastLine = lastLine;

  // 行内代码 `
  const backtickCount = (fixedLastLine.match(/(?<!\\)`/g) || []).length;
  if (backtickCount % 2 !== 0) {
    fixedLastLine += '`';
  }

  // 粗体 **
  const boldCount = (fixedLastLine.match(/(?<!\\)\*\*/g) || []).length;
  if (boldCount % 2 !== 0) {
    fixedLastLine += '**';
  }

  // 斜体 *（排除已匹配的 **）
  const allStars = (fixedLastLine.match(/(?<!\\)\*/g) || []).length;
  const pairedBoldStars = Math.floor((fixedLastLine.match(/(?<!\\)\*\*/g) || []).length / 1) * 2;
  const singleStars = allStars - pairedBoldStars;
  if (singleStars % 2 !== 0) {
    fixedLastLine += '*';
  }

  // 行内 LaTeX $（排除已匹配的 $$）
  const allDollars = (fixedLastLine.match(/(?<!\\)\$/g) || []).length;
  const pairedBlockDollars = (fixedLastLine.match(/(?<!\\)\$\$/g) || []).length * 2;
  const singleDollars = allDollars - pairedBlockDollars;
  if (singleDollars % 2 !== 0) {
    fixedLastLine += '$';
  }

  // 删除线 ~~
  const strikeCount = (fixedLastLine.match(/~~/g) || []).length;
  if (strikeCount % 2 !== 0) {
    fixedLastLine += '~~';
  }

  if (fixedLastLine !== lastLine) {
    result = result.slice(0, lastNewline + 1) + fixedLastLine;
  }

  return result;
}
