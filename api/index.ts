import app from '../server';

export default function handler(req: any, res: any) {
  // If Vercel rewrote /api/(.*) and stripped or altered the path, normalize it for Express
  if (req.url && !req.url.startsWith('/api')) {
    const original = req.headers['x-matched-path'] || req.headers['x-now-route-matches'];
    if (typeof original === 'string' && original.startsWith('/api')) {
      req.url = original;
    }
  }
  return app(req, res);
}
