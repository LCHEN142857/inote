export function ProjectCornerLink() {
  return (
    <a
      className="project-corner-link"
      href="https://github.com/LCHEN142857/inote"
      target="_blank"
      rel="noreferrer"
      aria-label="Open the project on GitHub"
      title="GitHub"
    >
      <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
        <path d="M12 0C5.37 0 0 5.37 0 12c0 5.3 3.44 9.79 8.2 11.38.6.11.82-.26.82-.58v-2.23c-3.34.73-4.04-1.6-4.04-1.6-.55-1.39-1.33-1.76-1.33-1.76-1.09-.74.08-.73.08-.73 1.2.09 1.84 1.23 1.84 1.23 1.07 1.83 2.8 1.31 3.49 1 .11-.78.42-1.31.76-1.61-2.67-.3-5.47-1.33-5.47-5.93 0-1.31.47-2.38 1.24-3.22-.12-.3-.54-1.52.12-3.17 0 0 1-.32 3.3 1.23a11.5 11.5 0 0 1 3-.4c1.03 0 2.06.13 3.01.4 2.29-1.55 3.3-1.23 3.3-1.23.65 1.65.24 2.87.12 3.17.77.84 1.24 1.91 1.24 3.22 0 4.61-2.8 5.63-5.47 5.93.43.38.8 1.1.8 2.22v3.27c0 .32.22.69.82.57C20.57 21.79 24 17.3 24 12 24 5.37 18.63 0 12 0z" />
      </svg>
      <span className="sr-only">GitHub</span>
    </a>
  );
}
