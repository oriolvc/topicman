/**
 * Sending and saving the YAML files collections and environments are exported to (ADR 0006).
 * The browser never parses them: the server reads and writes YAML.
 */

/** Request options that send a file's text as YAML instead of the JSON openapi-fetch sends. */
export function yamlBody<T>(text: string) {
  return {
    // The generated client types the body as the file's structure; it travels as the text itself.
    body: text as unknown as T,
    bodySerializer: (body: T) => body as unknown as string,
    headers: { 'Content-Type': 'application/yaml' },
  };
}

/** The file name the server suggests in `Content-Disposition`, or the fallback. */
export function fileNameOf(response: Response, fallback: string): string {
  const disposition = response.headers.get('Content-Disposition') ?? '';
  return /filename="([^"]+)"/.exec(disposition)?.[1] ?? fallback;
}

/** Hands a text to the browser as a download. */
export function saveFile(name: string, text: string): void {
  const url = URL.createObjectURL(new Blob([text], { type: 'application/yaml' }));
  const link = document.createElement('a');
  link.href = url;
  link.download = name;
  link.click();
  URL.revokeObjectURL(url);
}
