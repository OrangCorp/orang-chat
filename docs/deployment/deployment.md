## Notes
 1.There are two docker compose files, dev and prod. Dev features mailpit for local mail testing and local postgresql dbs, while prod assumes an external mail service and dbs

 2.Some of the services require https to work correctly

 3. The app has been tested, deployed and confirmed to be fully functional on an azure virtual machine with a publically available address and external supabase databases. There have also been attempts to deploy it in the azure container apps and azure kubernetes services, but they have been unsuccessful due to the limitations of the azure student subscription
## Deploying steps 
 1.Set up the caddyfile url to the appropriate value 
 2. Set up env varibles
 3. Use the appropriate docker compose
 4. Run "docker compose -f {file name} up -d --build"
 
## Dumping dev databases
 In order to dump database schemas and data from your currently running dev enviorment, run the dumper.sh file

