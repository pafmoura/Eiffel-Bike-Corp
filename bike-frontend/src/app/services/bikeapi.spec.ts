import { TestBed } from '@angular/core/testing';

import { Bikeapi } from './bikeapi';

describe('Bikeapi', () => {
  let service: Bikeapi;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(Bikeapi);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
