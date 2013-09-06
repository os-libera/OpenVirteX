"""
This is the H - figure topology that we are using for the NetVisor sprint 2 demo

Six switched connected in the shape of an H with 16 hosts 
connected to all the switches

host                host
|||                  ||
switch             switch
  |                  |
  | host             |
  |  ||              |    host
switch-------------switch ||| 
  |                  |
  |                  |
  |                  | 
switch             switch
  ||                 |||
host                  host
   host --- switch --- switch --- host

Adding the 'topos' dict with a key/value pair to generate our newly defined
topology enables one to pass in '--topo=mytopo' from the command line.
"""

from mininet.topo import Topo
from mininet.net import Mininet
from mininet.log import lg, setLogLevel
from mininet.cli import CLI
from mininet.node import RemoteController


class HTopo( Topo ):
    "H topology for netvisor demo."

    def __init__( self ):
        "Create custom topo."

        # Initialize topology
        Topo.__init__( self )

        # Add hosts

        # BS1
        topLeftHost1 = self.addHost( 'h1', ip='10.0.0.1' )
        middleLeftHost1 = self.addHost( 'h2', ip='10.0.0.2' )
        middleRightHost1 = self.addHost( 'h3', ip='10.0.0.3' )
        bottomRightHost1 = self.addHost( 'h4', ip='10.0.0.4' )

        #BS2
        topRightHost1 = self.addHost( 'h5', ip='10.0.0.1' )
        middleLeftHost2 = self.addHost( 'h6', ip='10.0.0.2' )
        bottomLeftHost1 = self.addHost( 'h7', ip='10.0.0.3' )

        #BS3
        topLeftHost2 = self.addHost( 'h8', ip='10.0.0.1' )
        bottomRightHost2 = self.addHost( 'h9', ip='10.0.0.2' )

        #FT1
        topRightHost2 = self.addHost( 'h10', ip='10.0.0.1' )
        middleRightHost2 = self.addHost( 'h11', ip='10.0.0.2' )
        bottomLeftHost2 = self.addHost( 'h12', ip='10.0.0.3' )
        
        #FT2
        topLeftHost3 = self.addHost( 'h13', ip='10.0.0.1' )
        bottomLeftHost3 = self.addHost( 'h14', ip='10.0.0.2' )
        topRightHost3 = self.addHost( 'h15', ip='10.0.0.3' )
        bottomRightHost3 = self.addHost( 'h16', ip='10.0.0.4' )

        # Add switches
        topLeftSwitch = self.addSwitch( 's1' )
        middleLeftSwitch = self.addSwitch( 's2' )
        bottomLeftSwitch = self.addSwitch( 's3' )

        topRightSwitch = self.addSwitch( 's4' )
        middleRightSwitch = self.addSwitch( 's5' )
        bottomRightSwitch = self.addSwitch( 's6' )

        # Add links between switches and hosts
        self.addLink( topLeftHost1, topLeftSwitch )
        self.addLink( topLeftHost2, topLeftSwitch )
        self.addLink( topLeftHost3, topLeftSwitch )

        self.addLink( middleLeftHost1, middleLeftSwitch )
        self.addLink( middleLeftHost2, middleLeftSwitch )

        self.addLink( bottomLeftHost1, bottomLeftSwitch )
        self.addLink( bottomLeftHost2, bottomLeftSwitch )
        self.addLink( bottomLeftHost3, bottomLeftSwitch )


        self.addLink( topRightHost1, topRightSwitch )
        self.addLink( topRightHost2, topRightSwitch )
        self.addLink( topRightHost3, topRightSwitch )


        self.addLink( middleRightHost1, middleRightSwitch )
        self.addLink( middleRightHost2, middleRightSwitch )

        self.addLink( bottomRightHost1, bottomRightSwitch )
        self.addLink( bottomRightHost2, bottomRightSwitch )
        self.addLink( bottomRightHost3, bottomRightSwitch )

        
        # Add links between switches
        self.addLink( topLeftSwitch, middleLeftSwitch )
        self.addLink( middleLeftSwitch, bottomLeftSwitch )
        self.addLink( middleLeftSwitch, middleRightSwitch )
        self.addLink( middleRightSwitch, bottomRightSwitch )
        self.addLink( middleRightSwitch, topRightSwitch )

topos = { 'htopo': ( lambda: HTopo() ) }


if __name__== '__main__':
    topo = HTopo()
    net = Mininet(topo, autoSetMacs=True)
    print "\nHosts configured with IPs, switches pointing to NetVisor port 6633\n"
    c0 = RemoteController( 'c0', ip='10.1.10.45' )
    net.controllers = [c0]
    net.start()
    CLI(net)
    net.stop()
