./ovxctl.py createNetwork tcp 10.1.10.48 10000 192.168.0.0 16
./ovxctl.py createVSwitch 1 1
./ovxctl.py createVSwitch 1 3
./ovxctl.py createVSwitch 1 5
./ovxctl.py createVLink 1 1/2-2/2,2/3-3/2
./ovxctl.py createVLink 1 3/3-4/2,4/3-5/2
./ovxctl.py connectHost 1 1 1 00:00:00:00:00:01
./ovxctl.py connectHost 1 3 1 00:00:00:00:00:03
./ovxctl.py connectHost 1 5 1 00:00:00:00:00:05
./ovxctl.py bootNetwork 1
