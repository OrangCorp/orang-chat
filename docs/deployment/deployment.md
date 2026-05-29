## Notes
# 1.There are two docker compose files, dev and prod. Dev features mailpit for local mail testing and local postgresql dbs, while prod assumes an external mail service and dbs
# 2.Some of the services require https to work correctly

## Deploying steps 
# 1.Set up the caddyfile url to the appropriate value 
# 2. Set up env varibles
# 3. Use the appropriate docker compose
# 4. Run "docker compose -f {file name} up -d --build"
 
## Dumping dev databases
# In order to dump database schemas and data from your currently running dev enviorment, run the dumper.sh file

