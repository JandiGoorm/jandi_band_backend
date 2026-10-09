FROM python:3.12-slim
RUN pip install --no-cache-dir requests==2.32.5 boto3==1.40.40 PyMySQL==1.1.2 cryptography==46.0.2
WORKDIR /tests
COPY run.py .
CMD ["python", "-u", "run.py"]
