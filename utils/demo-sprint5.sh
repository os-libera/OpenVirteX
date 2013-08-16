./ovxctl.py createNetwork tcp 192.168.56.5 10000 192.168.0.0 16
./ovxctl.py createVSwitch 1 [1]
./ovxctl.py createVSwitch 1 [3]
./ovxctl.py createVLink 1 1/2-2/2,2/3-3/2
./ovxctl.py connectHost 1 1 1 00:00:00:00:00:01
./ovxctl.py connectHost 1 3 1 00:00:00:00:00:03
./ovxctl.py bootNetwork 1

./ovxctl.py createNetwork tcp 192.168.56.5 20000 192.168.0.0 16
./ovxctl.py createVSwitch 2 [2]
./ovxctl.py createVSwitch 2 [4]
./ovxctl.py createVLink 2 2/3-3/2,3/3-4/2
./ovxctl.py connectHost 2 2 1 00:00:00:00:00:02
./ovxctl.py connectHost 2 4 1 00:00:00:00:00:04
./ovxctl.py bootNetwork 2
