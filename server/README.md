To run the server locally: 
1. Create a bridged Docker network named "custom0" 

$ docker network create "custom0"

2. Change permissions on the bash script 

$ chmod 770 buildDockeImage.sh 

3. Run the script 

$ sudo ./buildDockerImage.sh

4. Check that the server is running 

$ docker logs fleet-manager

5. Change API calls in the web-controller to http://localhost:1000
