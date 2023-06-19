import { RSocketClient } from 'rsocket-core';
import RSocketWebsocketClient from 'rsocket-websocket-client';

async function createClient(options) {
  console.log(RSocketClient);
  const client = new RSocketClient({
    setup: {
      dataMimeType: 'text/plain',
      keepAlive: 1000000, // avoid sending during test
      lifetime: 100000,
      metadataMimeType: 'text/plain',
    },
    transport: new RSocketWebsocketClient({
      host: options.host,
      port: options.port,
    }),
  });

  return client.connect();
}

async function run() {
  const rsocket = await createClient({
    host: '127.0.0.1',
    port: 8181,
  });
}

await run();
