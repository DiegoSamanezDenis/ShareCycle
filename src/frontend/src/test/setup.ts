import "@testing-library/jest-dom";

class MockEventSource {
  url: string;
  onmessage: ((event: MessageEvent) => void) | null = null;
  onerror: ((event: Event) => void) | null = null;
  readyState = 0;
  constructor(url: string) {
    this.url = url;
  }
  addEventListener() {
    // no-op mock
  }
  close() {
    this.readyState = 2;
  }
}

// @ts-expect-error test environment polyfill
global.EventSource = MockEventSource;
