import { Pipe, PipeTransform } from '@angular/core';
import { toFa } from './format';

/** Display Latin digits as Persian digits — for bare number bindings (counts, timers, order numbers). */
@Pipe({ name: 'faNum' })
export class FaNumPipe implements PipeTransform {
  transform(value: string | number | null | undefined): string {
    return toFa(value);
  }
}
