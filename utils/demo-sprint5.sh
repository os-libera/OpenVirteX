./ovxctl.py -n --hostname=192.168.56.1 createNetwork tcp 192.168.56.1 10000 192.168.0.0 16
./ovxctl.py -n --hostname=192.168.56.1 createSwitch 1 1
./ovxctl.py -n --hostname=192.168.56.1 createSwitch 1 2
./ovxctl.py -n --hostname=192.168.56.1 createLink 1 1/2-2/2
./ovxctl.py -n --hostname=192.168.56.1 connectHost 1 1 1 00:00:00:00:00:01
./ovxctl.py -n --hostname=192.168.56.1 connectHost 1 2 1 00:00:00:00:00:02
./ovxctl.py -n --hostname=192.168.56.1 startNetwork 1

#./ovxctl.py -n createNetwork tcp 192.168.56.5 20000 192.168.0.0 16
#./ovxctl.py -n createVSwitch 2 [2]
#./ovxctl.py -n createVSwitch 2 [4]
#./ovxctl.py -n createVLink 2 2/3-3/2,3/3-4/2
#./ovxctl.py -n connectHost 2 2 1 00:00:00:00:00:02
#./ovxctl.py -n connectHost 2 4 1 00:00:00:00:00:04
#./ovxctl.py -n bootNetwork 2
