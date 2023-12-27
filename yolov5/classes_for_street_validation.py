
import numpy as np

class BaseValidator:

    def __init__(self) -> None:
        pass

    def validate_track(self, track_array):
        pass


class Sremska(BaseValidator):

    def __init__(self, array_for_attrs) -> None:
        self.x1 = array_for_attrs[0]
        self.x2 = array_for_attrs[1]
        self.y1 = array_for_attrs[2]
        self.y2 = array_for_attrs[3]
        self.number_of_lains = 2
        
    def validate_track(self, track_array):
        return  self.x1 < track_array[0] < self.x2 and self.y1< track_array[1] < self.y2


class BulevarZoranaDjindjicaValidator(BaseValidator):

    def __init__(self, array_for_attrs) -> None:
        self.x1 = array_for_attrs[0]
        self.x2 = array_for_attrs[1]
        self.y1 = array_for_attrs[2]
        self.y2 = array_for_attrs[3]
        self.number_of_lains = 3

    def validate_track(self, track_array):
        return  self.x1 < track_array[0] < self.x2 and self.y1< track_array[1] < self.y2
    
class BulevarNemanjicaValidator(BaseValidator):

    def __init__(self, array_for_attrs) -> None:
        self.x1 = array_for_attrs[0]
        self.x2 = array_for_attrs[1]
        self.y1 = array_for_attrs[2]
        self.y2 = array_for_attrs[3]
        self.number_of_lains = 2

    def validate_track(self, track_array):
        return  self.x1 < track_array[0] < self.x2 and self.y1< track_array[1] < self.y2    
    
class VojvodeMisicaValidator(BaseValidator):

    def __init__(self, array_for_attrs) -> None:
        self.x1 = array_for_attrs[0]
        self.x2 = array_for_attrs[1]
        self.y1 = array_for_attrs[2]
        self.y2 = array_for_attrs[3]
        self.number_of_lains = 2

    def validate_track(self, track_array):
        return  self.x1 < track_array[0] < self.x2 and self.y1< track_array[1] < self.y2    
    
class BulevarNikoleTesleValidator(BaseValidator):

    def __init__(self, array_for_attrs) -> None:
        self.x1 = array_for_attrs[0]
        self.x2 = array_for_attrs[1]
        self.y1 = array_for_attrs[2]
        self.y2 = array_for_attrs[3]
        self.number_of_lains = 2

    def validate_track(self, track_array):
        return  self.x1 < track_array[0] < self.x2 and self.y1< track_array[1] < self.y2  



        




