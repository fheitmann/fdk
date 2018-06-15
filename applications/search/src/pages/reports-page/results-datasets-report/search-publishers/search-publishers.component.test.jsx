import React from 'react';
import { shallow } from 'enzyme';
import {SearchPublishers} from './search-publishers.component';

let onSearch;
let defaultProps;
let wrapper;

beforeEach(() => {
  onSearch = jest.fn();
  defaultProps = {
    onSearch,
    value: 'STAT'
  };
  wrapper = shallow(<SearchPublishers {...defaultProps} />);
});

test('should render SearchPublishers correctly with props', () => {
  expect(wrapper).toMatchSnapshot();
});