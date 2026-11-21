"""Local HTTP fixture for the notifications demo. Only client 42 exists."""

import json
from http.server import BaseHTTPRequestHandler, HTTPServer


class DemoClientHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path != "/api/clients/42":
            self.send_error(404, "Client de démonstration introuvable")
            return

        body = json.dumps({"id": 42, "email": "client.demo@bill-flex.test"}).encode("utf-8")
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)


if __name__ == "__main__":
    with HTTPServer(("127.0.0.1", 18083), DemoClientHandler) as server:
        server.serve_forever()
