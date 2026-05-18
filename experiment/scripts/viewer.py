#!/usr/bin/env python3
import http.server
import socketserver
import json
import os
import re
from pathlib import Path
from urllib.parse import urlparse, parse_qs


def parse_tasks_from_md(tasks_md_path):
    if not tasks_md_path.exists():
        return []
    content = tasks_md_path.read_text(errors='replace')
    tasks = []
    task_pattern = re.compile(r'^### (TASK-\d+): (.+)$', re.MULTILINE)
    status_pattern = re.compile(r'\*\*Status:\*\*\s+(\w+)')
    matches = list(task_pattern.finditer(content))
    for i, match in enumerate(matches):
        task_id = match.group(1)
        title = match.group(2).strip()
        section_start = match.end()
        section_end = matches[i + 1].start() if i + 1 < len(matches) else len(content)
        section = content[section_start:section_end]
        status_match = status_pattern.search(section)
        status = status_match.group(1) if status_match else 'NOT_STARTED'
        tasks.append({'task_id': task_id, 'title': title, 'status': status})
    return tasks

PORT = 8080
SCRIPTS_DIR = Path(__file__).parent.resolve()
EXPERIMENT_DIR = SCRIPTS_DIR.parent
ROOT_DIR = EXPERIMENT_DIR.parent

class ViewerHandler(http.server.SimpleHTTPRequestHandler):
    def do_GET(self):
        parsed_url = urlparse(self.path)
        path = parsed_url.path

        if path == '/':
            self.send_response(200)
            self.send_header("Content-type", "text/html")
            self.end_headers()
            viewer_html = (SCRIPTS_DIR / "viewer.html").read_bytes()
            self.wfile.write(viewer_html)
            return

        if path == '/api/state':
            self.handle_state(parsed_url.query)
            return
            
        if path == '/api/log':
            self.handle_log(parsed_url.query)
            return
            
        if path == '/api/file':
            self.handle_file(parsed_url.query)
            return
            
        if path == '/api/runs':
            self.handle_runs()
            return

        # Fallback to simple http server for any static assets if needed
        super().do_GET()

    def get_active_run_info(self, query_string=""):
        state_file = EXPERIMENT_DIR / "run_state.json"
        active_state = None
        if state_file.exists():
            try:
                active_state = json.loads(state_file.read_text())
            except Exception:
                pass
                
        query = parse_qs(query_string)
        tool_q = query.get("tool", [""])[0]
        model_q = query.get("model", [""])[0]
        
        if tool_q and model_q:
            if active_state and active_state.get("tool") == tool_q and active_state.get("model_short") == model_q:
                run_dir = ROOT_DIR / "runs" / tool_q / model_q / "experiment"
                return active_state, run_dir
                
            # Synthetic state for historical run
            state = {
                "status": "HISTORICAL",
                "tool": tool_q,
                "model_short": model_q,
                "tasks_done": "?",
                "tasks_total": 20
            }
            run_dir = ROOT_DIR / "runs" / tool_q / model_q / "experiment"
            return state, run_dir

        if active_state:
            tool = active_state.get("tool")
            model = active_state.get("model_short")
            if tool and model:
                run_dir = ROOT_DIR / "runs" / tool / model / "experiment"
                return active_state, run_dir
                
        return None, None

    def handle_state(self, query_string):
        state, run_dir = self.get_active_run_info(query_string)
        
        response = {
            "state": state,
            "run": None
        }
        
        if run_dir:
            run_json_path = run_dir / "run.json"
            if run_json_path.exists():
                try:
                    response["run"] = json.loads(run_json_path.read_text())
                    if state and state.get("status") == "HISTORICAL":
                        state["tasks_done"] = response["run"].get("tasks_done", "?")
                except Exception:
                    pass
                    
        # Build full ordered task list: TASKS.md order, enriched with run.json details
        all_tasks = []
        if run_dir:
            md_tasks = parse_tasks_from_md(run_dir.parent / "TASKS.md")
            completed = {t['task_id']: t for t in (response.get('run') or {}).get('tasks') or []}
            for md_task in md_tasks:
                tid = md_task['task_id']
                all_tasks.append(completed[tid] if tid in completed else md_task)
        response['all_tasks'] = all_tasks

        self.send_response(200)
        self.send_header("Content-type", "application/json")
        self.send_header("Access-Control-Allow-Origin", "*")
        self.end_headers()
        self.wfile.write(json.dumps(response).encode())

    def handle_log(self, query_string):
        _, run_dir = self.get_active_run_info(query_string)
        log_content = ""
        
        if run_dir:
            log_path = run_dir / "run.log"
            if log_path.exists():
                try:
                    # Tail the last 1000 lines approx
                    with open(log_path, 'rb') as f:
                        f.seek(0, os.SEEK_END)
                        filesize = f.tell()
                        blocksize = 1024 * 100 # Last 100KB
                        f.seek(max(0, filesize - blocksize))
                        log_content = f.read().decode('utf-8', errors='replace')
                except Exception:
                    log_content = "Error reading log."
        else:
            log_content = "No active run log found."
            
        self.send_response(200)
        self.send_header("Content-type", "text/plain")
        self.send_header("Access-Control-Allow-Origin", "*")
        self.end_headers()
        self.wfile.write(log_content.encode())

    def handle_file(self, query_string):
        query = parse_qs(query_string)
        task = query.get("task", [""])[0]
        attempt = query.get("attempt", [""])[0]
        file_type = query.get("type", [""])[0] # 'prompt' or 'failure'
        
        if not task or not attempt or not file_type:
            self.send_response(400)
            self.end_headers()
            return
            
        _, run_dir = self.get_active_run_info(query_string)
        content = "File not found."
        
        if run_dir:
            file_path = run_dir / task / f"attempt{attempt}_{file_type}.txt"
            if file_path.exists():
                try:
                    content = file_path.read_text(errors='replace')
                except Exception as e:
                    content = f"Error reading file: {e}"
        
        self.send_response(200)
        self.send_header("Content-type", "text/plain")
        self.send_header("Access-Control-Allow-Origin", "*")
        self.end_headers()
        self.wfile.write(content.encode())

    def handle_runs(self):
        runs = []
        runs_dir = ROOT_DIR / "runs"
        if runs_dir.exists():
            for tool_dir in runs_dir.iterdir():
                if tool_dir.is_dir():
                    for model_dir in tool_dir.iterdir():
                        if model_dir.is_dir():
                            if (model_dir / "experiment" / "run.json").exists():
                                runs.append({
                                    "tool": tool_dir.name,
                                    "model": model_dir.name
                                })
        
        self.send_response(200)
        self.send_header("Content-type", "application/json")
        self.send_header("Access-Control-Allow-Origin", "*")
        self.end_headers()
        self.wfile.write(json.dumps(runs).encode())

if __name__ == "__main__":
    with socketserver.TCPServer(("", PORT), ViewerHandler) as httpd:
        print(f"Serving live viewer at http://localhost:{PORT}")
        httpd.serve_forever()
