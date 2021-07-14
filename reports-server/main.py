from http.server import ThreadingHTTPServer
from reqhandler import APlusReportHandler
import reportsubmitter as ReportSubmitter
import logging, threading

def run_server(port=8888):
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(message)s", datefmt="%Y-%m-%d %H:%M:%S")

    threading.Thread(target=ReportSubmitter.report_submitter_thread).start()

    logging.info("Starting the HTTP server")
    server_inst = ThreadingHTTPServer(("", port), APlusReportHandler)

    try:
        logging.info(f"Listening on port {port}")
        server_inst.serve_forever()
    except KeyboardInterrupt:
        logging.info("Received keyboard interrupt signal")

    server_inst.server_close()
    logging.info("Server closed - shutting down the mail connection, please wait...")

    ReportSubmitter.signal_exit()

if __name__ == "__main__":
    run_server()
