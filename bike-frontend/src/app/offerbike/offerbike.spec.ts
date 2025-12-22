import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Offerbike } from './offerbike';

describe('Offerbike', () => {
  let component: Offerbike;
  let fixture: ComponentFixture<Offerbike>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Offerbike]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Offerbike);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
