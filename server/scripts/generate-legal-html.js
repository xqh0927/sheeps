const fs = require('fs');
const path = require('path');

function markdownToHtml(md) {
  let html = md;
  
  // Replace Windows line endings
  html = html.replace(/\r\n/g, '\n');
  
  // Escape HTML entities to prevent issues
  html = html
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');
    
  // Unescape back &gt; for blockquotes
  html = html.replace(/^&gt;\s+(.*)$/gm, '<blockquote><p>$1</p></blockquote>');
  
  // Headers
  html = html.replace(/^# (.*)$/gm, '<h1>$1</h1>');
  html = html.replace(/^## (.*)$/gm, '<h2>$1</h2>');
  html = html.replace(/^### (.*)$/gm, '<h3>$1</h3>');
  
  // Bold **text**
  html = html.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
  
  // Horizontal rules
  html = html.replace(/^---$/gm, '<hr>');
  
  // Lists
  html = html.replace(/^\-\s+(.*)$/gm, '<li>$1</li>');
  html = html.replace(/^\d+\.\s+(.*)$/gm, '<li>$1</li>');
  
  const lines = html.split('\n');
  let inList = false;
  let inBlockquote = false;
  const processedLines = [];
  
  for (let line of lines) {
    const trimmed = line.trim();
    
    // List handling
    if (trimmed.startsWith('<li>')) {
      if (!inList) {
        processedLines.push('<ul>');
        inList = true;
      }
    } else {
      if (inList) {
        processedLines.push('</ul>');
        inList = false;
      }
    }
    
    // Blockquote handling
    if (trimmed.startsWith('<blockquote>')) {
      if (!inBlockquote) {
        inBlockquote = true;
      }
      line = trimmed.replace('<blockquote><p>', '').replace('</p></blockquote>', '');
      processedLines.push('<blockquote><p>' + line + '</p></blockquote>');
      inBlockquote = false;
      continue;
    }
    
    if (trimmed === '') {
      processedLines.push('');
      continue;
    }
    
    const isTag = trimmed.startsWith('<h') || 
                  trimmed.startsWith('<ul') || 
                  trimmed.startsWith('</ul') || 
                  trimmed.startsWith('<li') || 
                  trimmed.startsWith('<hr') || 
                  trimmed.startsWith('<blockquote') || 
                  trimmed.startsWith('</blockquote');
    
    if (!isTag && trimmed.length > 0) {
      processedLines.push(`<p>${trimmed}</p>`);
    } else {
      processedLines.push(line);
    }
  }
  
  if (inList) {
    processedLines.push('</ul>');
  }
  
  return processedLines.join('\n');
}

function wrapHtml(title, htmlContent) {
  return `<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${title}</title>
    <style>
        body {
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
            line-height: 1.6;
            color: #333;
            background-color: #f7f9fa;
            margin: 0;
            padding: 24px 16px;
        }
        .container {
            max-width: 800px;
            margin: 0 auto;
            background: #fff;
            padding: 40px 32px;
            border-radius: 12px;
            box-shadow: 0 4px 12px rgba(0,0,0,0.05);
        }
        h1 {
            font-size: 28px;
            color: #111;
            border-bottom: 2px solid #eaeaea;
            padding-bottom: 12px;
            margin-top: 0;
        }
        h2 {
            font-size: 20px;
            color: #222;
            border-bottom: 1px solid #eee;
            padding-bottom: 8px;
            margin-top: 32px;
        }
        h3 {
            font-size: 16px;
            color: #333;
            margin-top: 24px;
        }
        p {
            margin: 16px 0;
        }
        ul, ol {
            padding-left: 24px;
        }
        li {
            margin: 8px 0;
        }
        blockquote {
            background-color: #f0f4f8;
            border-left: 4px solid #1976d2;
            margin: 20px 0;
            padding: 12px 20px;
            border-radius: 4px;
        }
        blockquote p {
            margin: 0;
        }
        strong {
            color: #000;
        }
        hr {
            border: 0;
            border-top: 1px solid #eee;
            margin: 32px 0;
        }
    </style>
</head>
<body>
    <div class="container">
        ${htmlContent}
    </div>
</body>
</html>`;
}

const legalDir = path.join(__dirname, '..', '..', 'docs', 'legal');
const privacyMd = fs.readFileSync(path.join(legalDir, '隐私政策.md'), 'utf8');
const agreementMd = fs.readFileSync(path.join(legalDir, '用户协议.md'), 'utf8');

const privacyHtml = wrapHtml('秘境消消乐 隐私政策', markdownToHtml(privacyMd));
const agreementHtml = wrapHtml('秘境消境乐 用户协议', markdownToHtml(agreementMd));

const outputTsPath = path.join(__dirname, '..', 'src', 'legal-docs.ts');
const fileContent = `// Automatically generated from legal markdown files. Do not edit manually.
export const privacyHtml = ${JSON.stringify(privacyHtml)};
export const agreementHtml = ${JSON.stringify(agreementHtml)};
`;

fs.writeFileSync(outputTsPath, fileContent, 'utf8');
console.log('Successfully generated legal-docs.ts');
