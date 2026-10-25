# Drishti — Accessibility Application for Visually Impaired Citizens

An AI-powered full-stack web and voice companion application built with React, Vite, Express, and Google Gemini.

---

## How to Run Locally on Your Computer

### Prerequisites
Make sure you have **Node.js** (version 18, 20, or higher) installed:
- Download Node.js from: https://nodejs.org/ (LTS version recommended)
- Verify in your terminal:
  ```bash
  node -v
  npm -v
  ```

---

### Step 1: Open Terminal in the Project Directory
In your command prompt or terminal:
```cmd
cd D:\Users\DELL\Downloads\drishti-accessibility-app
```

---

### Step 2: Install All Dependencies (Crucial Step)
Before running the application, install the packages:
```cmd
npm install
```
*(If your npm has strict peer dependency checking, you can run: `npm install --legacy-peer-deps`)*

---

### Step 3: Configure Environment Variables (Optional for AI)
1. Copy `.env.example` to a new file named `.env`:
   ```cmd
   copy .env.example .env
   ```
2. Open `.env` and add your Google Gemini API key:
   ```env
   GEMINI_API_KEY="your-gemini-api-key-here"
   ```
*(Note: Drishti has graceful offline fallbacks and built-in rules even if an API key is not provided.)*

---

### Step 4: Start the Local Development Server
Now run:
```cmd
npm run dev
```

You will see:
```text
Drishti full-stack server running on http://0.0.0.0:3000
```

---

### Step 5: Open in Your Browser
Open Chrome, Edge, or Brave and visit:
👉 **http://localhost:3000**

*Tip: Enable microphone and speaker permissions when prompted so voice navigation, wake-word ("Hey Drishti"), and continuous companion mode work seamlessly.*

---

## Deploying to Vercel

The project is pre-configured for Vercel with serverless API functions (`/api`) and static single-page app routing in `vercel.json`.

### Option A: Via Vercel CLI (Fastest)
1. Install Vercel CLI (if not installed):
   ```cmd
   npm install -g vercel
   ```
2. In your project directory, run:
   ```cmd
   vercel
   ```
3. Set your environment variable:
   ```cmd
   vercel env add GEMINI_API_KEY
   ```
4. Deploy to production:
   ```cmd
   vercel --prod
   ```

### Option B: Via GitHub & Vercel Dashboard
1. Push your project to a GitHub repository:
   ```cmd
   git init
   git add .
   git commit -m "Drishti Accessibility App"
   git branch -M main
   git remote add origin https://github.com/<your-username>/<repo-name>.git
   git push -u origin main
   ```
2. Go to [vercel.com/new](https://vercel.com/new) and import the repository.
3. Vercel automatically detects the settings from `vercel.json`:
   - **Framework Preset**: Vite
   - **Build Command**: `npm run build`
   - **Output Directory**: `dist`
   - **Install Command**: `npm install --legacy-peer-deps`
4. Under **Environment Variables**, add:
   - `GEMINI_API_KEY`: *(Your Google Gemini API Key)*
5. Click **Deploy**. Vercel will provide an `https://*.vercel.app` URL with full SSL and voice recognition support.

# drishti-ai-companion
