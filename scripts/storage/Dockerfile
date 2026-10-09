FROM python:3.12-slim
RUN apt-get update && apt-get install -y --no-install-recommends default-mysql-client \
    && rm -rf /var/lib/apt/lists/*
RUN pip install --no-cache-dir boto3==1.40.48 PyMySQL==1.1.2
WORKDIR /tools
COPY . .
ENTRYPOINT ["python"]
