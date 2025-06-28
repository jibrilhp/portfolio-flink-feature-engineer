FROM python:3.11-slim

WORKDIR /app

# Install system dependencies
RUN apt-get update && apt-get install -y \
    gcc \
    postgresql-client \
    && rm -rf /var/lib/apt/lists/*

# Copy requirements and install Python dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy application code
COPY data_generator.py .

# Create a non-root user
RUN useradd -m -u 1000 generator && chown -R generator:generator /app
USER generator

CMD ["python", "data_generator.py"]