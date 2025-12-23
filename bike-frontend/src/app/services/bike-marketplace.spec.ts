import { TestBed } from '@angular/core/testing';

import { BikeMarketplace } from './bike-marketplace';

describe('BikeMarketplace', () => {
  let service: BikeMarketplace;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(BikeMarketplace);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
