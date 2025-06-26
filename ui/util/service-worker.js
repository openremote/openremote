async function put(request, response, fallback) {
  // Only cache responses if the status code is 2xx
  // or if the status is 0 due to cors and the destination is an image
  if (response.ok || (response.status === 0 && request.destination === "image")) {
    const cache = await caches.open("openremote");
    cache.put(request, response.clone());
    return response;
  }
  return fallback();
}

async function tryCached(request, fallback) {
  if (request.method === "HEAD") {
    const cached = await caches.match(new Request(request.url, { method: "GET" }));
    if (cached) {
      return new Response(null, {
        status: cached.status || 200,
        headers: new Headers(cached.headers),
      });
    }
    return fallback();
  }

  const cached = await caches.match(request);
  if (cached) {
    return cached;
  }
  return fallback();
}

function cacheFirst(request) {
  return tryCached(request, async () => {
    const response = await fetch(request);
    return put(request, response, () => response);
  });
}

let version;
self.addEventListener("message", (event) => {
  if (typeof event.data === "object" && "version" in event.data) {
    version = event.data;
  }
});

self.addEventListener("fetch", (event) => {
  const { request } = event;
  const { origin, pathname } = new URL(request.url);

  console.debug("[SW]", request.method, pathname);

  if (["GET"].includes(request.method) && /^\/api\/\w+\/model\/getValueDescriptorSchema/.test(pathname)) {
    event.respondWith(cacheFirst(request));
    return;
  }
});
