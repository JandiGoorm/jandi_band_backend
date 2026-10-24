FROM python:3.15-slim-trixie
RUN pip install --no-cache-dir requests==2.34.2 boto3==1.43.118 PyMySQL==1.2.3 cryptography==50.0.2
WORKDIR /tests
COPY run.py .
CMD ["python", "-u", "run.py"]
