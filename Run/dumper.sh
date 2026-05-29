#!/bin/bash

DUMP_DIR="./postgres_dumps"
mkdir -p "$DUMP_DIR"

docker ps --filter "name=postgres" --format "{{.ID}}" | while read CONTAINER_ID; do
  CONTAINER_NAME=$(docker inspect --format='{{.Name}}' "$CONTAINER_ID" | sed 's/^\///')
   
  POSTGRES_USER=$(docker exec "$CONTAINER_ID" env | grep "^POSTGRES_USER=" | cut -d'=' -f2)
  POSTGRES_DB=$(docker exec "$CONTAINER_ID" env | grep "^POSTGRES_DB=" | cut -d'=' -f2)
  
  if [ -z "$POSTGRES_USER" ] || [ -z "$POSTGRES_DB" ]; then
    echo "Error: Could not determine POSTGRES_USER or POSTGRES_DB for $CONTAINER_NAME"
    continue
  fi
   
  docker exec "$CONTAINER_ID" pg_dump -U "$POSTGRES_USER" "$POSTGRES_DB" \
  --no-security-labels \
  --no-privileges \
  --no-owner \
  --inserts \
  | sed '/^\\restrict\|^\\unrestrict/d' \
  > "$DUMP_DIR/${CONTAINER_NAME}_${POSTGRES_DB}_$(date +%Y%m%d_%H%M%S).sql"

  echo "Dumped $CONTAINER_NAME.$POSTGRES_DB"
done
