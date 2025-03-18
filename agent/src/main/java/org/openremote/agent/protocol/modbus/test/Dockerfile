# Use an official lightweight Python image
FROM python:3.9-slim

# Set the working directory
WORKDIR /app

# Copy the requirements file and install dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy the Modbus server script into the image
COPY main.py .

# Expose the Modbus TCP port
EXPOSE 502

# Run the Modbus server
CMD ["python", "main.py"]
