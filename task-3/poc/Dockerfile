FROM python:3.12-slim

WORKDIR /app

COPY exporter/requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY exporter/exporter.py ./exporter.py

ENTRYPOINT ["python", "/app/exporter.py"]
