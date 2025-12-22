import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Mainlayout } from './mainlayout';

describe('Mainlayout', () => {
  let component: Mainlayout;
  let fixture: ComponentFixture<Mainlayout>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Mainlayout]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Mainlayout);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
