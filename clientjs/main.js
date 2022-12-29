import { RSocketConnector } from "rsocket-core";
import { WebsocketClientTransport } from "rsocket-websocket-client";
import { TcpClientTransport } from "rsocket-tcp-client";
import {Buffer} from 'buffer';

globalThis.Buffer = Buffer;

(async () => {
  const connector = webSocketConnector();
  const rSocket = await connector.connect();

  rSocket.fireAndForget({
    data: Buffer.from('hello'),
    // TODO: fix
    metadata: Buffer.from(JSON.stringify({
      route: 'fire-and-forget',
    }))
  }, {
    onError: e => console.error(e),
    onNext: (payload, isComplete) => {
      console.log(payload, isComplete);
    },
    onComplete() {
      console.log("Completed");
    }
  });

  function webSocketConnector() {
    return new RSocketConnector({
      transport: new WebsocketClientTransport({
        url: 'ws://localhost:8181/rsocket',
        wsCreator: (url) => new WebSocket(url),
      })
    })
  }

  function tcpConnector() {
    return new RSocketConnector({
      transport: new TcpClientTransport({
        connectionOptions: {
          host: '127.0.0.1',
          port: 8181
        }
      }),
    });
  }
})();
