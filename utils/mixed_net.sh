./ovxctl.py createNetwork tcp 10.1.10.48 10000 192.168.0.0 16
./ovxctl.py createVSwitch 1 1,2,3
./ovxctl.py createVSwitch 1 5
./ovxctl.py createVLink 1 3/3-4/2,4/3-5/2
./ovxctl.py connectHost 1 1 1 00:00:00:00:00:01
./ovxctl.py connectHost 1 2 1 00:00:00:00:00:02
./ovxctl.py connectHost 1 5 1 00:00:00:00:00:05
./ovxctl.py createVSwitchRoute 1 1 2/1 3/1 1/2-2/2
./ovxctl.py createVSwitchRoute 1 1 2/1 1/3 1/2-2/2,2/3-3/2
./ovxctl.py createVSwitchRoute 1 1 3/1 1/3 2/3-3/2
./ovxctl.py bootNetwork 1

./ovxctl.py createNetwork tcp 10.1.10.48 20000 192.168.0.0 16
./ovxctl.py createVSwitch 2 3
./ovxctl.py createVSwitch 2 4
./ovxctl.py createVSwitch 2 6
./ovxctl.py createVLink 2 3/3-4/2,4/3-5/2,5/3-6/2
./ovxctl.py createVLink 2 4/3-5/2,5/3-6/2
./ovxctl.py connectHost 2 3 1 00:00:00:00:00:03
./ovxctl.py connectHost 2 4 1 00:00:00:00:00:04
./ovxctl.py connectHost 2 6 1 00:00:00:00:00:06
./ovxctl.py bootNetwork 2
