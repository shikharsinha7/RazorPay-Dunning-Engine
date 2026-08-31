import { Component } from 'react';

// A render error in ANY one section (e.g. a chart choking on unexpected
// data) must not blank the rest of the dashboard. Error boundaries have to
// be class components — there's no hook equivalent — so this wraps each
// section individually in App.jsx rather than relying on one boundary for
// the whole page.
export default class ErrorBoundary extends Component {
  constructor(props) {
    super(props);
    this.state = { error: null };
  }

  static getDerivedStateFromError(error) {
    return { error };
  }

  componentDidCatch(error, info) {
    // eslint-disable-next-line no-console
    console.error(`[${this.props.label ?? 'section'}] render error:`, error, info);
  }

  render() {
    if (this.state.error) {
      return (
        <div className="rounded-lg border border-[var(--color-lost)]/40 bg-red-950/20 p-4 text-sm text-red-300">
          <strong>{this.props.label ?? 'This section'}</strong> failed to render: {this.state.error.message}
          <div className="mt-1 text-xs text-red-400/70">Check the browser console for the full stack trace.</div>
        </div>
      );
    }
    return this.props.children;
  }
}
