import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Myrentals } from './myrentals';

describe('Myrentals', () => {
  let component: Myrentals;
  let fixture: ComponentFixture<Myrentals>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Myrentals]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Myrentals);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
