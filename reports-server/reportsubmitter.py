import logging, os, smtplib
import config as MailConfig
from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText
from threading import Event
from reportmgr import REPORTS_PATH, ARCHIVE_PATH

exit_event = Event()

def _send_report(report_string):
    msg = MIMEMultipart()
    msg["From"] = MailConfig.FROM_ADDRESS
    msg["To"] = MailConfig.TO_ADDRESS
    msg["Subject"] = MailConfig.MAIL_SUBJECT
    msg.attach(MIMEText(report_string, "plain", "utf-8"))

    try:
        s = smtplib.SMTP(MailConfig.SERVER_ADDRESS, MailConfig.SERVER_PORT)
        s.login(MailConfig.SERVER_LOGIN, MailConfig.SERVER_PASSWORD)
        s.sendmail(msg["From"], msg["To"], msg.as_string())
        s.quit()
    except Exception as e:
        logging.error(f"Sending mail failed - {repr(e)}")
        return False

    return True

def _send_pending_reports():
    awaiting_reports = [f for f in os.listdir(REPORTS_PATH) if os.path.isfile(os.path.join(REPORTS_PATH, f))]
    mail_content_string = ""

    if not awaiting_reports:
        return

    for report_file in awaiting_reports:
        report_path = os.path.join(REPORTS_PATH, report_file)
        with open(report_path, "r", encoding="utf-8") as report:
            mail_content_string += f"<<< Report file {report_file} >>>\n{report.read().strip()}\n<<< End of report file >>>\n\n"

    mail_content_string = f"This is a report containing {len(awaiting_reports)} report{'s' if not len(awaiting_reports) == 1 else ''} from the A+ Courses plugin.\n\n{mail_content_string}"

    if _send_report(mail_content_string):
        for report_file in awaiting_reports:
            os.replace(os.path.join(REPORTS_PATH, report_file), os.path.join(REPORTS_PATH, ARCHIVE_PATH, report_file))
            logging.info(f"Report file {report_file} sent")

def signal_exit():
    exit_event.set()

def report_submitter_thread():
    logging.info("Starting the report submitter thread")
    while True:
        _send_pending_reports()
        if exit_event.wait(5): # sleep for 15 minutes
            logging.info("Report submitter thread stopped")
            return
