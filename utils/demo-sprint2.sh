./ovxctl.py createNetwork tcp 192.168.56.5 10000 192.168.0.0 16
./ovxctl.py createVSwitch 1 [1]
./ovxctl.py createVSwitch 1 [2]
./ovxctl.py createVLink 1 1/2-2/2
./ovxctl.py connectHost 1 1 1 00:00:00:00:00:01
./ovxctl.py connectHost 1 2 1 00:00:00:00:00:02
./ovxctl.py bootNetwork 1
