def to_dpid_str(switchId):
    return '00:' + ':'.join([("%x" % switchId)[i:i+2] for i in range(0, len(("%x" % switchId)), 2)])

def to_long_dpid(dpidStr):
    return int(dpidStr.replace(":",""),16)

class SubinterpreterExit(Exception):
  def __init__ (self, dropped = False):
    self.drop = dropped
    return
