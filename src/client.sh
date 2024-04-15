#!/bin/bash

# ClientApp launcher script

# Default settings
DEFAULT_HOSTNAME="localhost"
DEFAULT_PORT=1099

# Check if a custom hostname is provided
if [ -n "$1" ]; then
  HOSTNAME=$1
else
  HOSTNAME=$DEFAULT_HOSTNAME
fi

# Check if a custom port is provided
if [ -n "$2" ]; then
  PORT=$2
else
  PORT=$DEFAULT_PORT
fi

echo "Starting ClientApp connecting to $HOSTNAME on port $PORT..."

# Assuming ClientApp is compiled into the 'client' package
java -cp . client.ClientApp $HOSTNAME $PORT
