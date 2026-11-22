import { useMemo, useState } from 'react';
import { Check, Copy, Download, FileCode2, Search } from 'lucide-react';

const androidRawFiles = import.meta.glob(
  '/android-app/**/*.{kt,kts,xml,toml,properties,pro,md,bat}',
  {
    query: '?raw',
    import: 'default',
    eager: true,
  }
) as Record<string, string>;

interface AndroidCodeExplorerProps {
  isFa: boolean;
}

export function AndroidCodeExplorer({ isFa }: AndroidCodeExplorerProps) {
  const fileEntries = useMemo(() => {
    return Object.entries(androidRawFiles)
      .map(([path, content]) => ({
        path: path.replace(/^\//, ''),
        content,
      }))
      .sort((a, b) => a.path.localeCompare(b.path));
  }, []);

  const [query, setQuery] = useState('');
  const [selectedPath, setSelectedPath] = useState<string>(
    () => fileEntries.find((f) => f.path.endsWith('MainActivity.kt'))?.path ?? fileEntries[0]?.path ?? ''
  );
  const [copied, setCopied] = useState(false);

  const filteredFiles = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return fileEntries;
    return fileEntries.filter((f) => f.path.toLowerCase().includes(q));
  }, [fileEntries, query]);

  const activeFile = fileEntries.find((f) => f.path === selectedPath) ?? filteredFiles[0];

  const handleCopy = async () => {
    if (!activeFile) return;
    await navigator.clipboard.writeText(activeFile.content);
    setCopied(true);
    window.setTimeout(() => setCopied(false), 1800);
  };

  const handleDownload = () => {
    if (!activeFile) return;
    const blob = new Blob([activeFile.content], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = activeFile.path.split('/').pop() ?? 'source.kt';
    a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <section className="max-w-7xl mx-auto px-4 sm:px-6 py-8 space-y-5">
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-slate-900 dark:text-slate-100">
            {isFa ? 'کدبوم واقعی اندروید (Kotlin + Jetpack Compose)' : 'Native Android Studio Project (/android-app)'}
          </h2>
          <p className="text-sm text-slate-600 dark:text-slate-400 mt-1">
            {isFa
              ? `${fileEntries.length} فایل واقعی شامل Gradle، CameraX، MediaPipe Pose، Room، DataStore، Health Connect و تست‌های واحد.`
              : `${fileEntries.length} source files generated in /android-app using Kotlin, Jetpack Compose, CameraX, MediaPipe Pose Landmarker, Room, DataStore, and Health Connect.`}
          </p>
        </div>
        <div className="text-xs text-slate-500 dark:text-slate-400 max-w-md">
          {isFa
            ? 'محیط پیش‌نمایش کنونی مبتنی بر Node/Vite است؛ برای ساخت APK پروژه android-app را در Android Studio باز کنید.'
            : 'Environment note: this web preview runs in a Node/Vite container without JDK/Android SDK. Open /android-app in Android Studio to run Gradle build and device tests.'}
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-5 items-start">
        <div className="lg:col-span-4 rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 overflow-hidden">
          <div className="p-3 border-b border-slate-200 dark:border-slate-800 flex items-center gap-2">
            <Search className="w-4 h-4 text-slate-400 shrink-0" />
            <input
              type="text"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder={isFa ? 'جستجو در فایل‌های پروژه…' : 'Filter Android files…'}
              className="w-full bg-transparent text-sm text-slate-900 dark:text-slate-100 focus:outline-none"
            />
          </div>
          <div className="max-h-[540px] overflow-y-auto divide-y divide-slate-100 dark:divide-slate-800/70">
            {filteredFiles.map((file) => {
              const isSelected = activeFile?.path === file.path;
              const shortLabel = file.path.replace(/^android-app\//, '');
              return (
                <button
                  key={file.path}
                  type="button"
                  onClick={() => setSelectedPath(file.path)}
                  className={`w-full text-start px-3.5 py-2.5 text-xs font-mono flex items-center gap-2 transition-colors ${
                    isSelected
                      ? 'font-semibold text-white'
                      : 'text-slate-700 dark:text-slate-300 hover:bg-slate-50 dark:hover:bg-slate-800/70'
                  }`}
                  style={isSelected ? { backgroundColor: 'var(--accent)' } : undefined}
                >
                  <FileCode2 className="w-3.5 h-3.5 shrink-0 opacity-80" />
                  <span className="truncate">{shortLabel}</span>
                </button>
              );
            })}
          </div>
        </div>

        <div className="lg:col-span-8 rounded-2xl border border-slate-200 dark:border-slate-800 bg-slate-950 text-slate-100 overflow-hidden">
          <div className="px-4 py-3 border-b border-slate-800 flex items-center justify-between gap-3">
            <span className="text-xs font-mono text-slate-300 truncate">
              {activeFile?.path ?? ''}
            </span>
            <div className="flex items-center gap-2 shrink-0">
              <button
                type="button"
                onClick={handleCopy}
                className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-md text-xs font-medium bg-slate-800 hover:bg-slate-700 text-slate-200 whitespace-nowrap shrink-0"
              >
                {copied ? <Check className="w-3.5 h-3.5" /> : <Copy className="w-3.5 h-3.5" />}
                {copied ? (isFa ? 'کپی شد' : 'Copied') : (isFa ? 'کپی' : 'Copy')}
              </button>
              <button
                type="button"
                onClick={handleDownload}
                className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-md text-xs font-medium bg-slate-800 hover:bg-slate-700 text-slate-200 whitespace-nowrap shrink-0"
              >
                <Download className="w-3.5 h-3.5" />
                {isFa ? 'دانلود' : 'Download'}
              </button>
            </div>
          </div>
          <pre
            dir="ltr"
            className="p-4 text-xs leading-relaxed overflow-auto max-h-[540px] font-mono text-slate-200"
          >
            <code>{activeFile?.content ?? ''}</code>
          </pre>
        </div>
      </div>
    </section>
  );
}
