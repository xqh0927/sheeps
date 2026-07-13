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
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", "Helvetica Neue", Arial, sans-serif;
            line-height: 1.7;
            color: #334155;
            background-color: #ffffff;
            margin: 0;
            padding: 24px 16px;
        }
        .container {
            max-width: 800px;
            margin: 0 auto;
            background: #ffffff;
            padding: 32px 24px;
            border-radius: 12px;
            border: 1px solid #e2e8f0;
        }
        h1 {
            font-size: 26px;
            color: #0f172a;
            border-bottom: 2px solid #f1f5f9;
            padding-bottom: 16px;
            margin-top: 0;
            margin-bottom: 24px;
            text-align: center;
        }
        h2 {
            font-size: 18px;
            color: #1e3a8a;
            border-left: 4px solid #2563eb;
            padding-left: 12px;
            margin-top: 32px;
            margin-bottom: 16px;
        }
        h3 {
            font-size: 15px;
            color: #1e293b;
            margin-top: 24px;
            margin-bottom: 12px;
        }
        p {
            margin: 14px 0;
        }
        ul, ol {
            padding-left: 20px;
            margin: 12px 0;
        }
        li {
            margin: 8px 0;
        }
        strong {
            color: #0f172a;
        }
        blockquote {
            background-color: #eff6ff;
            border-left: 4px solid #3b82f6;
            margin: 20px 0;
            padding: 14px 20px;
            border-radius: 8px;
            color: #1e40af;
        }
        blockquote p {
            margin: 0;
        }
        hr {
            border: 0;
            border-top: 1px solid #e2e8f0;
            margin: 32px 0;
        }
        /* 现代扁平表格设计 */
        .table-wrapper {
            overflow-x: auto;
            margin: 20px 0;
            border-radius: 8px;
            border: 1px solid #e2e8f0;
        }
        table {
            width: 100%;
            border-collapse: collapse;
            font-size: 14px;
            text-align: left;
        }
        th, td {
            padding: 12px 16px;
            border-bottom: 1px solid #e2e8f0;
        }
        th {
            background-color: #f8fafc;
            color: #1e293b;
            font-weight: 600;
        }
        tr:last-child td {
            border-bottom: none;
        }
        tr:hover {
            background-color: #f8fafc;
        }
        /* 状态高亮与标签 */
        .status-ok {
            color: #059669;
            font-weight: 600;
        }
        .status-part {
            color: #d97706;
            font-weight: 600;
        }
        .tag {
            display: inline-block;
            background: #eff6ff;
            color: #2563eb;
            padding: 2px 10px;
            border-radius: 12px;
            font-size: 12px;
            margin: 2px;
            font-weight: 500;
        }
        .footer {
            text-align: center;
            color: #64748b;
            font-size: 13px;
            margin-top: 40px;
            line-height: 1.8;
        }
        /* 针对移动端优化 padding */
        @media (max-width: 600px) {
            body {
                padding: 12px 8px;
            }
            .container {
                padding: 24px 16px;
                border-radius: 12px;
            }
            th, td {
                padding: 10px 12px;
            }
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

const publicDir = path.join(__dirname, '..', 'public');
fs.mkdirSync(publicDir, { recursive: true });
fs.writeFileSync(path.join(publicDir, 'privacy.html'), privacyHtml, 'utf8');
fs.writeFileSync(path.join(publicDir, 'agreement.html'), agreementHtml, 'utf8');
console.log('Successfully generated server/public/privacy.html & agreement.html');
