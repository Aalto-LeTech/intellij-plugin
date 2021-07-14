import json, logging, os
from datetime import datetime
from pytz import timezone

REPORTS_PATH = "reports"
ARCHIVE_PATH = "archive"

def _get_string_safe(report_data, key):
    entry = report_data.get(key)
    if not entry or not isinstance(entry, str):
        return "<unknown>"

    return entry

def save_report(client_address, report_string):
    # create reports directories if necessary
    if not os.path.exists(REPORTS_PATH):
        os.mkdir(REPORTS_PATH)

    if not os.path.exists(os.path.join(REPORTS_PATH, ARCHIVE_PATH)):
        os.mkdir(os.path.join(REPORTS_PATH, ARCHIVE_PATH))

    report_data = json.loads(report_string)
    active_plugins = report_data["loadedPlugins"]
    stack_traces = report_data["stackTraces"]

    if not isinstance(active_plugins, list) or not isinstance(stack_traces, list):
        logging.warn("Malformed JSON report")
        return False

    # the file name is the current UTC timestamp in milliseconds
    file_name = str(round(datetime.utcnow().timestamp() * 1000)) + ".txt"
    file_path = os.path.join(REPORTS_PATH, file_name)
    logging.info(f"Writing report to {file_path}")

    with open(file_path, "w", encoding="utf-8") as report_file:
        report_file.write(f"Client IP address: {client_address}\n")
        report_file.write(f"Report date: {datetime.now(timezone('Europe/Helsinki')).strftime('%Y-%m-%d %H:%M:%S')} (Finnish time)\n\n")
        report_file.write(f"IntelliJ version: {_get_string_safe(report_data, 'ideProduct')} ({_get_string_safe(report_data, 'ideVersion')})\n")
        report_file.write(f"Operating system version: {_get_string_safe(report_data, 'osName')} ({_get_string_safe(report_data, 'osVersion')})\n")
        report_file.write(f"JVM version: {_get_string_safe(report_data, 'jvmName')} ({_get_string_safe(report_data, 'jvmVersion')})\n\n")
        report_file.write(f"Plugin version: {_get_string_safe(report_data, 'pluginVersion')}\n")
        report_file.write(f"Last action performed by user: {_get_string_safe(report_data, 'lastAction')}\n")
        report_file.write(f"Additional error details: {_get_string_safe(report_data, 'errorInfo')}\n\n")
        report_file.write(f"Active plugins: (count: {len(active_plugins)}):\n")
        for plugin in active_plugins:
            report_file.write(f"  > {_get_string_safe(plugin, 'name')} ({_get_string_safe(plugin, 'id')})\n")

        report_file.write("\n")
        report_file.write(f"Stack traces (count: {len(stack_traces)}):\n")
        for stack_trace in stack_traces:
            stack_trace = stack_trace.replace('\r\n', '\n') # replace Windows line endings with Unix line endings for consistency

            report_file.write("=================== Begin stack trace ===================\n")
            report_file.write(f"{stack_trace}\n")
            report_file.write("==================== End stack trace ====================\n")

    return True
