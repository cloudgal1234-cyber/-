// Vercel serverless function — GitHub proxy
// Keeps GITHUB_PAT secret on the server side.
// GET  /api/data        → read data/family-story.json from GitHub
// POST /api/data        → write data/family-story.json to GitHub

const OWNER  = 'cloudgal1234-cyber';
const REPO   = '-';
const FILE   = 'data/family-story.json';
const BRANCH = 'main';

module.exports = async (req, res) => {
  res.setHeader('Access-Control-Allow-Origin',  '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');
  if (req.method === 'OPTIONS') { res.status(200).end(); return; }

  const pat = process.env.GITHUB_PAT;
  if (!pat) {
    res.status(500).json({ error: 'GITHUB_PAT environment variable is not set in Vercel.' });
    return;
  }

  const apiUrl = `https://api.github.com/repos/${OWNER}/${REPO}/contents/${FILE}`;
  const baseHeaders = {
    'Authorization': `token ${pat}`,
    'Accept':        'application/vnd.github.v3+json',
    'User-Agent':    'family-story-app'
  };

  // ── GET: read current data ──────────────────────────────────────────
  if (req.method === 'GET') {
    try {
      const r = await fetch(apiUrl, { headers: baseHeaders });
      if (r.status === 404) { res.json({ data: null, sha: null }); return; }
      if (!r.ok) { const e = await r.json(); res.status(r.status).json({ error: e.message }); return; }
      const j = await r.json();
      const data = JSON.parse(Buffer.from(j.content.replace(/\n/g, ''), 'base64').toString('utf-8'));
      res.json({ data, sha: j.sha });
    } catch (e) {
      res.status(500).json({ error: e.message });
    }
    return;
  }

  // ── POST: save data ─────────────────────────────────────────────────
  if (req.method === 'POST') {
    try {
      const { dbData, sha } = req.body;
      if (!dbData) { res.status(400).json({ error: 'dbData is required' }); return; }

      const content = Buffer.from(JSON.stringify(dbData, null, 2)).toString('base64');
      const putBody  = { message: 'ספר משפחה — עדכון נתונים 📖', content, branch: BRANCH };
      if (sha) putBody.sha = sha;

      const doPut = async (body) => fetch(apiUrl, {
        method: 'PUT',
        headers: { ...baseHeaders, 'Content-Type': 'application/json' },
        body: JSON.stringify(body)
      });

      let r = await doPut(putBody);

      // SHA conflict — refresh SHA and retry once
      if (r.status === 409) {
        const fresh = await fetch(apiUrl, { headers: baseHeaders });
        if (fresh.ok) {
          const fj = await fresh.json();
          putBody.sha = fj.sha;
          r = await doPut(putBody);
        }
      }

      if (!r.ok) { const e = await r.json(); res.status(r.status).json({ error: e.message }); return; }
      const result = await r.json();
      res.json({ sha: result.content.sha });
    } catch (e) {
      res.status(500).json({ error: e.message });
    }
    return;
  }

  res.status(405).json({ error: 'Method not allowed' });
};
