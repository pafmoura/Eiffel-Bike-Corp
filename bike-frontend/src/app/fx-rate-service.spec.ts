import { TestBed } from '@angular/core/testing';

import { FxRateService } from './fx-rate-service';

describe('FxRateService', () => {
  let service: FxRateService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(FxRateService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
