#mkdir overlay.local
#mkdir overlay.local/upper
#mkdir overlay.local/work
#mkdir bin
#
#fuse-overlayfs -o lowerdir=build/classes/java:build/resources,upperdir=overlay.local/upper,workdir=overlay.local/work bin

while [ 1 ]
do
    sleep 1
	cp .vscode/launch.bak.json .vscode/launch.json
done
