const { RSocketClient } = require('rsocket-core');
const RSocketWebsocketClient = require('rsocket-websocket-client');
const WebSocket = require('ws');

async function createClient(options) {
  const transportOptions = {
    url: 'ws://`${options.host}`:${options.port}',
    // In non-browser environments we need to provide a
    // factory method which can return an instance of a
    // websocket object. Browsers however, have this
    // functionality built-in.
    wsCreator: (url) => {
      return new WebSocket(url);
    }
  };
  const setupOptions = {
    keepAlive: 1000000,
    lifetime: 100000,
    dataMimeType: 'text/plain',
    metadataMimeType: 'text/plain'
  };
  const transport = new RSocketWebsocketClient(transportOptions);
  const client = new RSocketClient({ setup: setupOptions, transport });
  return await client.connect();
}

async function run() {
  const rsocket = await createClient({
    host: '127.0.0.1',
    port: 9898,
  });
}

run();
