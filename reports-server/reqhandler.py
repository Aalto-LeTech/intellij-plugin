from http.server import BaseHTTPRequestHandler
import reportmgr as ReportManager
import logging

class APlusReportHandler(BaseHTTPRequestHandler):
    def _respond_with_string(self, message, status_code=200):
        self.send_response(status_code)
        self.send_header("Content-Type", "text/plain; charset=utf-8")
        self.end_headers()

        self.wfile.write(message.encode("utf-8"))

    def setup(self):
        self.timeout = 10
        BaseHTTPRequestHandler.setup(self)

    def do_POST(self):
        self.close_connection = True
        if self.path == "/report":
            if "Content-Length" in self.headers:
                client_address = self.headers["X-Forwarded-For"] if "X-Forwarded-For" in self.headers else self.client_address[0]
                logging.info(f"Incoming report from {client_address}")

                data_length = int(self.headers["Content-Length"])
                data = self.rfile.read(data_length).decode("utf-8")

                if ReportManager.save_report(client_address, data):
                    logging.info("Report written successfully")
            else:
                self._respond_with_string("Request is missing Content-Length (chunked encoding is not supported)", 400)
        else:
            self._respond_with_string("Resource not found.", 404)
