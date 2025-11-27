FROM python:3.11-slim

WORKDIR /app

COPY pyproject.toml ./
RUN pip install --no-cache-dir -e .

COPY app ./app
COPY README.md ./README.md

EXPOSE 8000 8080

CMD ["python", "-m", "app"]
